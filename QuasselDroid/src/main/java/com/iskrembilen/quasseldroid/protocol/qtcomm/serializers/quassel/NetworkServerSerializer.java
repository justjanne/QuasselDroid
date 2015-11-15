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

import com.iskrembilen.quasseldroid.protocol.state.NetworkServer;
import com.iskrembilen.quasseldroid.protocol.qtcomm.DataStreamVersion;
import com.iskrembilen.quasseldroid.protocol.qtcomm.EmptyQVariantException;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QDataInputStream;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QDataOutputStream;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QMetaTypeRegistry;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QMetaTypeSerializer;
import com.iskrembilen.quasseldroid.protocol.qtcomm.QVariant;

import java.io.IOException;
import java.util.Map;

public class NetworkServerSerializer implements QMetaTypeSerializer<NetworkServer> {

    @Override
    public void serialize(QDataOutputStream stream, NetworkServer data,
                          DataStreamVersion version) throws IOException {
        throw new IOException("NetworkServerSerializer.serialize() unimplemented!");
    }

    @SuppressWarnings("unchecked")
    @Override
    public NetworkServer deserialize(QDataInputStream stream, DataStreamVersion version) throws IOException, EmptyQVariantException {
        Map<String, QVariant<?>> map = (Map<String, QVariant<?>>)
                QMetaTypeRegistry.instance().getTypeForName("QVariantMap").getSerializer().deserialize(stream, version);

        return new NetworkServer((String) map.get("Host").getData(),
                (Long) map.get("Port").getData(),
                (String) map.get("Password").getData(),

                (Boolean) map.get("UseSSL").getData(),
                (Integer) map.get("sslVersion").getData(),

                (Boolean) map.get("UseProxy").getData(),
                (String) map.get("ProxyHost").getData(),
                (Long) map.get("ProxyPort").getData(),
                (Integer) map.get("ProxyType").getData(),
                (String) map.get("ProxyUser").getData(),
                (String) map.get("ProxyPass").getData()
        );

    }

}
