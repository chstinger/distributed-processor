package com.aiiot.distributed.processor.bean;

import javax.lang.model.element.Element;

/**
 * Created : 05/08/2023-13.17
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public record FieldTypeInfo(FieldType fieldType, FieldTypeInfo listType, String objectName, TypeInfo[] typeInfo) {

    public static FieldTypeInfo create(String typeInfoField) {

        FieldType fieldType = getFieldType(typeInfoField);
        if (fieldType==FieldType.List) {
            return new FieldTypeInfo(fieldType, FieldTypeInfo.create(getListType(typeInfoField)),null,new TypeInfo[1]);
        }
        if (fieldType==FieldType.Object)
            return new FieldTypeInfo(fieldType, null,typeInfoField,new TypeInfo[1]);
        return new FieldTypeInfo(fieldType, null,null,null);
    }


    private static String getListType(String listType) {
        return listType.replaceFirst("java.util.List<","").replace(">","");
    }


    private static FieldType getFieldType(String typeInfo) {
        if (typeInfo.startsWith("java.util.List"))
            return FieldType.List;
        if (typeInfo.equals("boolean"))
            return FieldType.Boolean;
        if (typeInfo.equals("Boolean"))
            return FieldType.BooleanNull;
        if (typeInfo.equals("long"))
            return FieldType.Long;
        if (typeInfo.equals("Long"))
            return FieldType.LongNull;
        if (typeInfo.equals("int"))
            return FieldType.Int;
        if (typeInfo.equals("Integer"))
            return FieldType.IntNull;
        if (typeInfo.equals("java.time.LocalDateTime"))
            return FieldType.LocalDateTime;
        // Todo local date time
        if (typeInfo.equals("java.lang.String"))
            return FieldType.String;
        return FieldType.Object;
    }

    @Override
    public String toString() {
        return "FieldTypeInfo{" +
                "fieldType=" + fieldType +
                ", listType=" + listType +
                ", objectName='" + objectName + '\'' +
                '}';
    }

    public String getStreamType() {
        return switch (fieldType) {
            case Long -> "Long";
            case LongNull -> "Long";
            case Int -> "Int";
            case IntNull -> "Int";
            case BooleanNull -> "Boolean";
            case Boolean -> "Boolean";
            case String -> "UTF";
            case DateTime -> null;
            case LocalDateTime -> null;
            case List -> null;
            case Object -> null;
        };

    }

    public String getStreamListType() {
        return switch (fieldType) {
            case Long -> "Long";
            case LongNull -> "Long";
            case Int -> "Int";
            case IntNull -> "Int";
            case BooleanNull -> "Boolean";
            case Boolean -> "Boolean";
            case String -> "String";
            case DateTime -> "DateTime";
            case LocalDateTime -> "LocalDateTime";
            case List -> null;
            case Object -> typeInfo[0].className();
        };

    }

    public String write(String name) {
        if (fieldType()==FieldType.List) {
            return """  
                    {   
                        if (commonData.__FIELD__!=null) {
                            dataOutputStream.writeInt(commonData.__FIELD__.size());
                            for (__class__ c :commonData.__FIELD__) {
                                  __write__               
                            }
                        } else {
                            dataOutputStream.writeInt(-1);
                        }
                    }
                    """.replaceAll("__FIELD__",name).replaceAll("__class__",listType.getStreamListType()).replaceAll("__write__",listType.writeField("c"));

        }
        return writeField("commonData."+name);

    }

    private String writeField(String name) {
        if (fieldType()==FieldType.Object) {
            TypeInfo typeInfo=typeInfo()[0];
            if (typeInfo.isEnum()) {
                return "dataOutputStream.writeInt("+name+".ordinal());\n";
            } else {
                return "CommonStreamer.writeInstance(dataOutputStream,"+name+");\n";
            }
        }
        if (fieldType()==FieldType.String)
            return "dataOutputStream.writeUTF(" + name + "!=null?" + name + ":\"\");\n";
        if (fieldType()==FieldType.LocalDateTime)
            return "dataOutputStream.writeLong(" + name + "!=null?" + name + ".toInstant(ZoneOffset.UTC).toEpochMilli():-1L);\n";

        return "dataOutputStream.write" + getStreamType() + "(" + name + ");\n";
    }

    public String read(String name) {
        if (fieldType()==FieldType.List) {
            return """  
                    {
                        int listLength = dataInputStream.readInt();
                        if (listLength>=0) {
                            commonData.__FIELD__=new ArrayList<__class__>();
                            for (int i = 0; i < listLength; i++) {
                                commonData.__FIELD__.add(__read__);
                            }
                        }
                    }
                    """.replaceAll("__FIELD__",name).replaceAll("__class__",listType.getStreamListType()).replaceAll("__read__",listType.read());
        }
        if (fieldType()==FieldType.Object) {
            TypeInfo typeInfo=typeInfo()[0];
            if (typeInfo.isEnum()) {
                return "commonData."+name+"="+typeInfo.className()+".values()[dataInputStream.readInt()];\n";
            } else {
                return "commonData."+name+"=("+typeInfo.className()+")Common.readInstance(dataInputStream);\n";
            }
        }
        if (fieldType()==FieldType.LocalDateTime) {
            return """  
                    {
                        long epochMilli = dataInputStream.readLong();
                        if (epochMilli==-1)
                            commonData.__FIELD__=null;
                        else
                            commonData.__FIELD__= LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
                    }
                    """.replaceAll("__FIELD__",name);

            // Todo add localDateTime read to CommonStreamer
        }
        return "commonData."+name+"=dataInputStream.read"+getStreamType()+"();\n";
    }


    public String read() {
        if (fieldType()==FieldType.Object) {
            TypeInfo typeInfo=typeInfo()[0];
            if (typeInfo.isEnum()) {
                return typeInfo.className()+".values()[dataInputStream.readInt()]";
            } else {
                return "("+typeInfo.className()+")CommonStreamer.readInstance(dataInputStream)";
            }
        }
        // Todo support date
        return "dataInputStream.read"+getStreamType()+"()";
    }
}
