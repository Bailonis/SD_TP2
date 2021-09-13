package tp1.api;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * <p>
 * A class to perform service discovery, based on periodic service contact
 * endpoint announcements over multicast communication.
 * </p>
 *
 * <p>
 * Servers announce their *name* and contact *uri* at regular intervals. The
 * server actively collects received announcements.
 * </p>
 *
 * <p>
 * Service announcements have the following format:
 * </p>
 *
 * <p>
 * &lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;
 * </p>
 */
public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}

	// The pre-aggreed multicast endpoint assigned to perform discovery.
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;
	static final int DEAD_PERIOD = 10000;

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";
	private static final String POINTS = ":";
	private InetSocketAddress addr;
	private String serviceName;
	private String serviceURI;
	private String newDomainName;
	public HashMap<String, DomainInfo> record;

	/**
	 * @param serviceName the name of the service to announce
	 * @param serviceURI  an uri string - representing the contact endpoint of the
	 *                    service being announced
	 */
	public Discovery(String serviceName, String serviceURI, String domain) throws UnknownHostException {
		this.addr = DISCOVERY_ADDR;
		this.serviceName = serviceName;
		this.serviceURI = serviceURI;
		this.newDomainName = domain;
		this.record = new HashMap<String, DomainInfo>();

	}

	/**
	 * Starts sending service announcements at regular intervals...
	 */
	public void start() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName, serviceURI));

		byte[] announceBytes = String.format("%s%s%s%s%s", newDomainName, POINTS, serviceName, DELIMITER, serviceURI)
				.getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			MulticastSocket ms = new MulticastSocket(addr.getPort());
			ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));

			// start thread to send periodic announcements
			new Thread(() -> {
				for (;;) {
					try {
						ms.send(announcePkt);
						Thread.sleep(DISCOVERY_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
						// do nothing
					}
				}
			}).start();

			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				DomainInfo info;
				String uri;
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);
						String msg = new String(pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(DELIMITER);

						if (msgElems.length == 2) {
							// periodic announcement

							System.out.printf("FROM %s (%s) : %s\n", pkt.getAddress().getCanonicalHostName(),
									pkt.getAddress().getHostAddress(), msg);
							// TODO: to complete by recording the received information from the other node.
							String aux = msgElems[0];
							String domainName = aux.split(":")[0];
							String s = aux.split(":")[1];

							uri = msgElems[1];// aqui
							if (!s.equals(this.serviceName)) {

								info = record.get(domainName);

								if (info == null) {
									record.put(domainName, new DomainInfo(uri));
								} else
									info.setTime();
							}
						}

					} catch (IOException e) {
						// do nothing
					}
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the known servers for a service.
	 *
	 * @param serviceName the name of the service being discovered
	 * @return an array of URI with the service instances discovered.
	 * @throws URISyntaxException
	 *
	 */
	public DomainInfo knownUrisOf(String serviceName) throws URISyntaxException {

		return this.record.get(serviceName);
	}

}
