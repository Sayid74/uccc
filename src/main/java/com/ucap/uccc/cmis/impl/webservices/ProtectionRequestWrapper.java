/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ucap.uccc.cmis.impl.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.chemistry.opencmis.commons.impl.MimeHelper;

/**
 * This request wrapper checks if the request is a multipart request (required
 * by MTOM) and checks if the first part is not bigger than the provide max
 * size. This protects the Web Services endpoint from oversized XML attacks.
 */
public class ProtectionRequestWrapper extends HttpServletRequestWrapper {

    private static final String MULTIPART = "multipart/";
    private static final byte CR = 0x0D;
    private static final byte LF = 0x0A;
    private static final byte DASH = 0x2D;

    private final int messageMax;
    private final InputStream orgStream;
    private final ServletInputStream checkedStream;
    private final byte[] boundary;

    public ProtectionRequestWrapper(HttpServletRequest request, int max) throws ServletException {
        super(request);

        // check multipart
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART)) {
            throw new ServletException("Invalid multipart request!");
        }

        // get boundary
        boundary = MimeHelper.getBoundaryFromMultiPart(contentType);
        if (boundary == null) {
            throw new ServletException("Invalid multipart request!");
        }

        // set up checked stream
        try {
            messageMax = max;
            orgStream = super.getInputStream();
            checkedStream = new CheckServletInputStream();
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return checkedStream;
    }

    class CheckServletInputStream extends ServletInputStream {

        private final int streamMax;
        private byte[] linebuffer;
        private int pos;
        private int count;
        private int boundariesFound;

        public CheckServletInputStream() {
            streamMax = messageMax + 2 * (boundary.length + 6);
            linebuffer = new byte[64 * 1024];
            pos = 0;
            count = 0;
            boundariesFound = 0;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
        }

        @Override
        public synchronized void reset() throws IOException {
        }

        @Override
        public int available() throws IOException {
            return orgStream.available();
        }

        @Override
        public int read() throws IOException {
            int b = orgStream.read();

            if (boundariesFound == 2) {
                return b;
            }

            addToLineBuffer((byte) b);

            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }

            int r = orgStream.read(b, off, len);

            if (boundariesFound == 2) {
                return r;
            }

            addToLineBuffer(b, off, r);

            return r;
        }

        @Override
        public long skip(long n) throws IOException {
            if (n <= 0) {
                return 0;
            }

            return read(new byte[(n > 8 * 1024 ? 8 * 1024 : (int) n)]);
        }

        @Override
        public void close() throws IOException {
            orgStream.close();
        }

        private void checkBoundary(int startPos) {
            int lastStartPos = 0;
            for (int i = startPos; i < pos; i++) {
                if (linebuffer[i] == LF) {
                    if (countBoundaries(lastStartPos, i)) {
                        return;
                    }

                    lastStartPos = i + 1;
                }
            }

            if (lastStartPos > 0) {
                if (lastStartPos == pos) {
                    pos = 0;
                } else {
                    System.arraycopy(linebuffer, lastStartPos, linebuffer, 0, pos - lastStartPos);
                    pos = pos - lastStartPos;
                }
            }
        }

        private boolean countBoundaries(int startPos, int newLinePos) {
            if (isBoundary(startPos, newLinePos)) {
                boundariesFound++;

                if (boundariesFound == 2) {
                    // found boundary a second time within the size
                    // limit -> request is ok, no more checks necessary
                    linebuffer = null;
                }
            }

            return boundariesFound > 1;
        }

        private boolean isBoundary(int startPos, int newLinePos) {
            // a boundary consists of two dashes, the boundary and a CR
            // -> boundary line length == boundary length + three characters
            if (newLinePos - startPos == boundary.length + 3) {
                if (linebuffer[startPos] == DASH && linebuffer[startPos + 1] == DASH
                        && linebuffer[startPos + boundary.length + 2] == CR) {

                    for (int i = 0; i < boundary.length; i++) {
                        if (linebuffer[startPos + i + 2] != boundary[i]) {
                            return false;
                        }
                    }

                    return true;
                }
            }

            return false;
        }

        /**
         * Adds a byte to the line buffer.
         */
        private void addToLineBuffer(byte b) throws IOException {
            if (pos == linebuffer.length) {
                expandBuffer(1);
            }

            linebuffer[pos++] = (byte) b;

            if (b == LF) {
                checkBoundary(pos - 1);
            }

            if (boundariesFound < 2) {
                count++;
                if (count > streamMax) {
                    throw new IOException("SOAP message too big!");
                }
            }
        }

        /**
         * Adds a buffer to the line buffer.
         */
        private void addToLineBuffer(byte[] b, int off, int len) throws IOException {
            if (len <= 0) {
                return;
            }

            if (pos + len >= linebuffer.length) {
                expandBuffer(len);
            }

            System.arraycopy(b, off, linebuffer, pos, len);
            pos += len;

            checkBoundary(pos - len);

            if (boundariesFound < 2) {
                count += len;
                if (count > streamMax) {
                    throw new IOException("SOAP message too big!");
                }
            }
        }

        /**
         * Expand the line buffer.
         */
        private void expandBuffer(int len) throws IOException {
            if (pos + len > streamMax) {
                throw new IOException("SOAP message too big!");
            }

            int expand = (len < 64 * 1024 ? 64 * 1024 : len);
            byte[] newBuffer = new byte[linebuffer.length + expand];
            System.arraycopy(linebuffer, 0, newBuffer, 0, pos);
            linebuffer = newBuffer;
        }

		@Override
		public boolean isFinished() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isReady() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setReadListener(ReadListener listener) {
			// TODO Auto-generated method stub
			
		}
    }
}
