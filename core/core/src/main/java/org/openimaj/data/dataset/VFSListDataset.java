package org.openimaj.data.dataset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.io.IOUtils;
import org.openimaj.io.ObjectReader;
import org.openimaj.util.array.ArrayIterator;

/**
 * A {@link ListDataset} backed by a directory of items (either locally or
 * remotely), or items stored in a compressed archive.
 * <p>
 * As an example, this class can be used to easily create a {@link ListDataset}
 * from a directory of images:
 * 
 * <pre>
 * ListDataset&lt;FImage&gt; dataset = new VFSListDataset&lt;FImage&gt;(&quot;/path/to/directory/of/images&quot;,
 * 		ImageUtilities.FIMAGE_READER);
 * </pre>
 * 
 * a zip file of images:
 * 
 * <pre>
 * ListDataset&lt;FImage&gt; dataset = new VFSListDataset&lt;FImage&gt;(
 * 		&quot;zip:file:/path/to/images.zip&quot;, ImageUtilities.FIMAGE_READER);
 * </pre>
 * 
 * or even a remote zip of images hosted via http:
 * 
 * <pre>
 * ListDataset&lt;FImage&gt; dataset = new VFSListDataset&lt;FImage&gt;(
 * 		&quot;zip:http://localhost/&tilde;jsh2/thumbnails.zip&quot;, ImageUtilities.FIMAGE_READER);
 * </pre>
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 * @param <INSTANCE>
 *            The type of instance in the dataset
 */
public class VFSListDataset<INSTANCE> extends ReadableListDataset<INSTANCE> implements Identifiable {
	private FileObject[] files;
	private FileObject base;

	/**
	 * Construct a list dataset from any virtual file system source (local
	 * directory, remote zip file, etc).
	 * 
	 * @see "http://commons.apache.org/proper/commons-vfs/filesystems.html"
	 * @param path
	 *            the file system path or uri. See the Apache Commons VFS2
	 *            documentation for all the details.
	 * @param reader
	 *            the {@link ObjectReader} that reads the data from the VFS
	 * @throws FileSystemException
	 *             if an error occurs accessing the VFS
	 */
	public VFSListDataset(final String path, final ObjectReader<INSTANCE> reader) throws FileSystemException {
		super(reader);

		final FileSystemManager fsManager = VFS.getManager();
		base = fsManager.resolveFile(path);

		files = base.findFiles(new FileSelector() {

			@Override
			public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
				return true;
			}

			@Override
			public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
				if (fileInfo.getFile().getType() == FileType.FILE) {
					final BufferedInputStream stream = new BufferedInputStream(fileInfo.getFile().getContent()
							.getInputStream());

					try {
						return IOUtils.canRead(reader, stream, fileInfo.getFile().getName().getBaseName());
					} finally {
						if (stream != null)
							stream.close();
					}
				}

				return false;
			}
		});
	}

	/**
	 * Get the underlying file descriptors of the files in the dataset
	 * 
	 * @return the array of file objects
	 */
	public FileObject[] getFileObjects() {
		return files;
	}

	/**
	 * Get the underlying file descriptor for a particular instance in the
	 * dataset.
	 * 
	 * @param index
	 *            index of the instance
	 * 
	 * @return the file object corresponding to the instance
	 */
	public FileObject getFileObject(int index) {
		return files[index];
	}

	@Override
	public INSTANCE getInstance(int index) {
		try {
			return read(files[index]);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int numInstances() {
		return files.length;
	}

	private INSTANCE read(FileObject file) throws IOException {
		return reader.read(file.getContent().getInputStream());
	}

	@Override
	public Iterator<INSTANCE> iterator() {
		return new Iterator<INSTANCE>() {
			ArrayIterator<FileObject> filesIterator = new ArrayIterator<FileObject>(files);

			@Override
			public boolean hasNext() {
				return filesIterator.hasNext();
			}

			@Override
			public INSTANCE next() {
				try {
					return read(filesIterator.next());
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void remove() {
				filesIterator.remove();
			}
		};
	}

	@Override
	public String getID(int index) {
		try {
			return base.getName().getRelativeName(files[index].getName());
		} catch (final FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s(%d instances)", this.getClass().getName(), this.files.length);
	}

	@Override
	public String getID() {
		return base.getName().getBaseName();
	}
}