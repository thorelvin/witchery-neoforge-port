import com.sun.tools.attach.VirtualMachine;

public final class OriginalWitcheryAttach {
    private OriginalWitcheryAttach() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected: <pid> <agent-jar> <plan-file>");
        }
        VirtualMachine target = VirtualMachine.attach(args[0]);
        try {
            target.loadAgent(args[1], args[2]);
        } finally {
            target.detach();
        }
    }
}
