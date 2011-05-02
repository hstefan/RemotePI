/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bhft;

/**
 *
 * @author hstefan
 */
public class FileIsNotOnTreeException extends Exception {

    /**
     * Creates a new instance of <code>FileIsNotOnTreeException</code> without detail message.
     */
    public FileIsNotOnTreeException() {
    }

    /**
     * Constructs an instance of <code>FileIsNotOnTreeException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public FileIsNotOnTreeException(String filename) {
        super(filename + "doesn't exists on file tree");
    }
}
