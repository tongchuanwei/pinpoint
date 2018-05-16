/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.thrift.io;

import java.util.ArrayList;
import java.util.List;

import com.navercorp.pinpoint.io.header.ByteArrayHeaderReader;
import com.navercorp.pinpoint.io.header.Header;
import com.navercorp.pinpoint.io.header.HeaderReader;
import com.navercorp.pinpoint.io.header.InvalidHeaderException;
import com.navercorp.pinpoint.thrift.dto.ThriftRequest;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * copy->TBaseDeserializer
 */
public class HeaderTBaseDeserializer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TProtocol protocol;
    private final TMemoryInputTransport trans;
    private final TBaseLocator locator;

    /**
     * Create a new TDeserializer. It will use the TProtocol specified by the
     * factory that is passed in.
     *
     * @param protocolFactory Factory to create a protocol
     */
    HeaderTBaseDeserializer(TProtocolFactory protocolFactory, TBaseLocator locator) {
        this.trans = new TMemoryInputTransport();
        this.protocol = protocolFactory.getProtocol(trans);
        this.locator = locator;
    }

    /**
     * Deserialize the Thrift object from a byte array.
     *
     * @param bytes   The array to read from
     */
    public TBase<?, ?> deserialize(byte[] bytes) throws TException {

        try {
            trans.reset(bytes, 0, bytes.length);
            return readInternal();
        } finally {
            trans.clear();
            protocol.reset();
        }
    }

    private TBase<?, ?> readInternal() throws TException {

        Header header = readHeader();

        TBase<?, ?> base = locator.tBaseLookup(header.getType());
        base.read(protocol);
        return base;
    }

    private Header readHeader() throws TException {
        HeaderReader reader = newHeaderReader();
        Header header = readHeader(reader);
        skipHeaderOffset(reader);
        return header;
    }

    private void skipHeaderOffset(HeaderReader reader) {
        trans.reset(trans.getBuffer(), reader.getOffset(), reader.getRemaining());
    }

    private HeaderReader newHeaderReader() {
        byte[] buffer = trans.getBuffer();
        int bufferPosition = trans.getBufferPosition();
        int bytesRemainingInBuffer = trans.getBytesRemainingInBuffer();
        return new ByteArrayHeaderReader(buffer, bufferPosition, bytesRemainingInBuffer);
    }

    private Header readHeader(HeaderReader reader) throws TException {
        try {
            return reader.readHeader();
        } catch (InvalidHeaderException e) {
            throw new TException("invalid header Caused by:" + e.getMessage(), e);
        }
    }

    public List<TBase<?, ?>> deserializeList(byte[] buffer) throws TException {
        final List<TBase<?, ?>> tBaseList = new ArrayList<TBase<?,?>>();

        try {
            trans.reset(buffer, 0, buffer.length);
            while (trans.getBytesRemainingInBuffer() > 0) {

                    final TBase<?, ?> tBase = readInternal();
                    tBaseList.add(tBase);

            }
        } finally {
            trans.clear();
            protocol.reset();
        }
        return tBaseList;
    }

    public ThriftRequest deserializeThrfitRequest(byte[] bytes) throws TException {

        try {
            trans.reset(bytes, 0, bytes.length);
            return readThrfitRequest();
        } finally {
            trans.clear();
            protocol.reset();
        }
    }

    private ThriftRequest readThrfitRequest() throws TException {
        Header header = readHeader();

        TBase<?, ?> base = locator.tBaseLookup(header.getType());
        base.read(protocol);

        return new ThriftRequest(header, base);
    }



}