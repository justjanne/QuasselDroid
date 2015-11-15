/*
    QuasselDroid - Quassel client for Android
    Copyright (C) 2015 Ken Børge Viktil
    Copyright (C) 2015 Magnus Fjell
    Copyright (C) 2015 Martin Sandsmark <martin.sandsmark@kde.org>

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

package com.iskrembilen.quasseldroid.protocol.qtcomm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class QVariant<T> implements Serializable {
    T data;
    DataStreamVersion version;
    QVariantType type = QVariantType.Invalid;
    String userTypeName = null;

    public QVariant(T data, String userType) {
        this.data = data;
        this.type = QVariantType.UserType;
        this.userTypeName = userType;
    }

    public QVariant(T data, QVariantType t) {
        this.data = data;
        this.type = t;
    }

    private void clear() {
        data = null;
    }

    public QVariant() {

    }

    public QVariantType getType() {
        return type;
    }

    public T getData() throws EmptyQVariantException {
        if (data == null)
            throw new EmptyQVariantException();

        return data;
    }

    public boolean isValid() {
        return (type != QVariantType.Invalid &&
                data != null);
    }

    public static class QVariantSerializer<U> implements QMetaTypeSerializer<QVariant<U>> {
        public QVariantSerializer() {

        }

        @SuppressWarnings("unchecked")
        @Override
        public QVariant<U> deserialize(QDataInputStream src, DataStreamVersion version) throws IOException, EmptyQVariantException {
            int type = (int) src.readUInt(32);
            if (version.getValue() < DataStreamVersion.Qt_4_0.getValue()) {
                //FIXME: Implement?
                /*if (u >= MapFromThreeCount)
		            return;
		        u = map_from_three[u];
				 */
            }
            boolean is_null = false;
            if (version.getValue() >= DataStreamVersion.Qt_4_2.getValue())
                is_null = src.readUnsignedByte() != 0;

            QVariant<U> ret = new QVariant<U>();
            if (type == QVariantType.UserType.value) {
                String name = (String) QMetaTypeRegistry.instance().getTypeForId(QMetaType.Type.QByteArray.getValue()).getSerializer().deserialize(src, version);
                name = name.trim();
                ret.userTypeName = name;

                try {
                    type = QMetaTypeRegistry.instance().getIdForName(name);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Corrupt data, unable to deserialize this: '" + name + "'");
                }
            }


//			for(QVariantType tpe : QVariantType.values()){
//				if(tpe.getValue() == type){
//					ret.type = tpe;
//					break;
//				}
//			}

            ret.type = QVariantType.getByValue(type); //Replaced the iteration shot above, this is much more efficient

            if (ret.type == QVariantType.Invalid) {// || is_null) { //includes data = null; FIXME: is this correct?
                // Since we wrote something, we should read something
                QMetaTypeRegistry.instance().getTypeForId(QMetaType.Type.QString.getValue()).getSerializer().deserialize(src, version);
                ret.data = null;
                return ret;
            }
            //Unchecked cast so we can read unknown qvariants at run time and then inspect the contents
            if (ret.type == QVariantType.UserType) {
                ret.data = (U) QMetaTypeRegistry.instance().getTypeForName(ret.userTypeName).getSerializer().deserialize(src, version);
            } else {
                ret.data = (U) QMetaTypeRegistry.instance().getTypeForId(type).getSerializer().deserialize(src, version);
            }
            return ret;
        }

        @Override
        public void serialize(QDataOutputStream stream, QVariant<U> data, DataStreamVersion version) throws IOException {
            stream.writeUInt(data.type.getValue(), 32);
            if (version.getValue() < DataStreamVersion.Qt_4_0.getValue()) {
                //FIXME: Implement?
            }
//			if (version.getValue() >= DataStreamVersion.Qt_4_2.getValue())
            stream.writeByte(data == null ? 1 : 0);

            if (data.type == QVariantType.UserType) {
//				QMetaTypeRegistry.instance().getTypeForId(QMetaType.Type.QString.getValue()).getSerializer().serialize(stream, data.getUserTypeName(), version);
                QMetaTypeRegistry.instance().getTypeForId(QMetaType.Type.QByteArray.getValue()).getSerializer().serialize(stream, data.getUserTypeName(), version);
                QMetaTypeRegistry.instance().getTypeForName(data.getUserTypeName()).getSerializer().serialize(stream, data.data, version);
            } else {
                QMetaTypeRegistry.instance().getTypeForId(data.type.getValue()).getSerializer().serialize(stream, data.data, version);
            }
        }

    }

    public String getUserTypeName() {
        return userTypeName;
        //TODO: Implement user types
    }

    public String toString() {
        switch (type) {
            case ByteArray:
            case String:
            case CString:
                return (String) data;
            case UInt:
            case Int:
            case Bool:
                return data.toString();
            case Map:
                StringBuilder ret = new StringBuilder("( ");
                Map<Object, Object> map = (Map<Object, Object>) data;
                for (Map.Entry<Object, Object> element : map.entrySet()) {
                    ret.append(element.getKey().toString());
                    ret.append(" : ");
                    ret.append(element.getValue().toString());
                    ret.append(", ");
                }
                ret.append(" )");
                return ret.toString();
            case StringList:
            case List:
                StringBuilder r = new StringBuilder("( ");
                List<Object> list = (List<Object>) data;
                for (Object o : list) {
                    r.append(o.toString());
                    r.append(", ");
                }
                r.append(" )");
                return r.toString();
            case UserType:
                return data.toString();
            case Time:
            case Date:
            case DateTime:
                return ((Calendar) data).getTime().toGMTString();
            default:
                return "/" + type.toString() + " [ " + data.toString() + " ]/";
        }
    }

    public String niceString() {
        StringBuilder r = new StringBuilder("QVariant(");
        if (type==QVariantType.UserType && userTypeName!=null && !userTypeName.trim().equals(""))
            r.append(userTypeName);
        else
            r.append(type.value);
        r.append(", ");
        r.append(toString());
        r.append(")");
        return r.toString();
    }
}
