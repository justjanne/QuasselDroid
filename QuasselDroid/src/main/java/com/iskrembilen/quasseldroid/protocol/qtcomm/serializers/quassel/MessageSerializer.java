/**
 QuasselDroid - Quassel client for Android
 Copyright (C) 2011 Martin Sandsmark <martin.sandsmark@kde.org>

 This program is free software: you can redistribute it and/or modify it
 under the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 3 of the License, or (at your option)
 any later version, or under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either version 2.1 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License and the
 GNU Lesser General Public License along with this program.  If not, see
 <http://www.gnu.org/licenses/>.
 */

package com.iskrembilen.quasseldroid.protocol.qtcomm.serializers.quassel;

import android.text.SpannableString;

import com.iskrembilen.quasseldroid.protocol.state.BufferInfo;
import com.iskrembilen.quasseldroid.protocol.state.IrcMessage;
import com.iskrembilen.quasseldroid.protocol.qtcomm.DataStreamVersion;
import com.iskrembilen.quasseldroid.protocol.qtcomm.EmptyQVariantException;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QDataInputStream;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QDataOutputStream;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QMetaTypeRegistry;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QMetaTypeSerializer;

import java.io.IOException;
import java.util.Date;

public class MessageSerializer implements QMetaTypeSerializer<IrcMessage> {

    @Override
    public void serialize(QDataOutputStream stream, IrcMessage data,
                          DataStreamVersion version) throws IOException {
        stream.writeInt(data.messageId);
        stream.writeUInt(data.timestamp.getTime() / 1000, 32);
        stream.writeUInt(data.type.getValue(), 32);
        stream.writeByte(data.flags);
        stream.writeBytes(data.getSender());// TODO FIXME
        stream.writeBytes(data.content.toString());//ditto
    }

    @Override
    public IrcMessage deserialize(QDataInputStream stream,
                                  DataStreamVersion version) throws IOException, EmptyQVariantException {
        IrcMessage ret = new IrcMessage();
        ret.messageId = stream.readInt();
        ret.timestamp = new Date(stream.readUInt(32) * 1000);
        ret.type = IrcMessage.Type.getForValue((int) stream.readUInt(32));
        ret.flags = stream.readByte();
        ret.bufferInfo = (BufferInfo) QMetaTypeRegistry.instance().getTypeForName("BufferInfo").getSerializer().deserialize(stream, version);
        ret.setSender((String) QMetaTypeRegistry.instance().getTypeForName("QByteArray").getSerializer().deserialize(stream, version));
        ret.content = SpannableString.valueOf((String) QMetaTypeRegistry.instance().getTypeForName("QByteArray").getSerializer().deserialize(stream, version));

        return ret;
    }

}
