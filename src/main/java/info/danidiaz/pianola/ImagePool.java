package info.danidiaz.pianola;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImagePool {

    /*Some multimap class from Guava or Apache Commons would be better here,
    but I want to avoid dependencies.*/
    private Map<Dimension,ArrayList<BufferedImage>> dimIndexedMultimap;
    
    public ImagePool() {
        this.dimIndexedMultimap = new HashMap<Dimension, ArrayList<BufferedImage>>();
    }
    
/*    public ImagePool(Collection<BufferedImage> imageColl) {
       this();
       
       for (BufferedImage image: imageColl) {
           Dimension d = getDimension(image);
           if (!dimIndexedMultimap.containsKey(d)) {
               dimIndexedMultimap.put(d,new LinkedList<BufferedImage>());
           } 
           dimIndexedMultimap.get(d).add(image);
       }
    }*/
    
    private static Dimension getDimension(BufferedImage image) {
        return new Dimension(image.getWidth(), image.getHeight());
    }
    
/*    public void flush() {
        for (List<BufferedImage> imageList: this.dimIndexedMultimap.values()) {
            for (BufferedImage image: imageList) {
                image.flush();
            }
        }
        this.dimIndexedMultimap.clear();
    }*/
    
    public BufferedImage obtainImage(Dimension d) {
        if (this.dimIndexedMultimap.containsKey(d)) {
            List<BufferedImage> imageList = this.dimIndexedMultimap.get(d);
            BufferedImage image = imageList.remove(0);
            if (imageList.isEmpty()) {
            	this.dimIndexedMultimap.remove(d);
            }
            return image;
        } 
        return new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
    }
    
    public void releaseImage(BufferedImage image) {
    	Dimension d = getDimension(image);
    	if (this.dimIndexedMultimap.containsKey(d)) {
            List<BufferedImage> imageList = this.dimIndexedMultimap.get(d);
            imageList.add(image);
    	} else {
    		this.dimIndexedMultimap.put(d, 
    				new ArrayList<BufferedImage>(Collections.singletonList(image)));
    	}
    }

    public JsonNode asJson() {
      	ArrayNode array = JsonNodeFactory.instance.arrayNode();
      	for (Map.Entry<Dimension, ArrayList<BufferedImage>> entry : 
      			this.dimIndexedMultimap.entrySet())
      	{
      		ObjectNode node = JsonNodeFactory.instance.objectNode();
      		node.put("width", entry.getKey().width);
      		node.put("height", entry.getKey().height);
      		node.put("count", entry.getValue().size());
      		array.add(node);
      	}
    	return array;
    }
}
