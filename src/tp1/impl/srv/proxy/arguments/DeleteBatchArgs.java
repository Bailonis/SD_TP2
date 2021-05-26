package tp1.impl.srv.proxy.arguments;

import java.util.List;

public class DeleteBatchArgs {

    final List<DeleteArgs> entries;

    public DeleteBatchArgs(List<DeleteArgs> entries){
        this.entries = entries;
    }
    
}
