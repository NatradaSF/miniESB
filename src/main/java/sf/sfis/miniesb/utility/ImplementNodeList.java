package sf.sfis.miniesb.utility;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImplementNodeList implements NodeList {
	    private final List<Node> nodes;

	    public ImplementNodeList(List<Node> nodes) {
	        this.nodes = nodes;
	    }

	    @Override
	    public Node item(int index) {
	        return (index >= 0 && index < nodes.size()) ? nodes.get(index) : null;
	    }

	    @Override
	    public int getLength() {
	        return nodes.size();
	    }
}
