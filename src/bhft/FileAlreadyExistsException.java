/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bhft;

/**
 *
 * @author hstefan
 */
public class FileAlreadyExistsException extends Exception {

    public FileAlreadyExistsException(String filename) {
        super(filename + " alread exists on file tree.");
    }
}
