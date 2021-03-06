/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.core.fs.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.flink.annotation.Internal;
import org.apache.flink.core.fs.FSDataOutputStream;

/**
 * The <code>LocalDataOutputStream</code> class is a wrapper class for a data
 * output stream to the local file system.
 */
@Internal
public class LocalDataOutputStream extends FSDataOutputStream {

	private static final int MAX_OPEN_TRIES = 3;
	
	/**
	 * The file output stream used to write data.
	 */
	private FileOutputStream fos;

	/**
	 * Constructs a new <code>LocalDataOutputStream</code> object from a given {@link File} object.
	 * 
	 * @param file
	 *        the {@link File} object the data stream is read from
	 * @throws IOException
	 *         thrown if the data output stream cannot be created
	 */
	public LocalDataOutputStream(final File file) throws IOException {
		// we allow multiple tries to create the file, to increase resilience against spurious I/O failures
		
		FileNotFoundException lastException = null;
		
		for (int attempt = 0; attempt < MAX_OPEN_TRIES; attempt++) {
			try {
				this.fos = new FileOutputStream(file);
				return;
			}
			catch (FileNotFoundException e) {
				lastException = e;
			}
		}
		throw lastException;
	}

	@Override
	public void write(final int b) throws IOException {
		fos.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		fos.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		fos.close();
	}


	@Override
	public void flush() throws IOException {
		fos.flush();
	}

	@Override
	public void sync() throws IOException {
		fos.getFD().sync();
	}
}
