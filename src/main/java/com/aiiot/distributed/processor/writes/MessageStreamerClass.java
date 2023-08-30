package com.aiiot.distributed.processor.writes;

import com.aiiot.distributed.processor.bean.ClassWriter;
import com.aiiot.distributed.processor.bean.Field;
import com.aiiot.distributed.processor.bean.TypeInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created : 30/07/2023-22.19
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public class MessageStreamerClass extends ClassWriter {


    private final TypeInfo typeInfo;
    private final int version;
    private LinkedHashMap<String, String> replaceMap;

    public MessageStreamerClass(TypeInfo typeInfo,int version) {

        super(typeInfo.getPackageName(), typeInfo.className()+"Stream", false, getImports(typeInfo,new String[]{
                        "java.io.DataInputStream" ,
                        "java.io.DataOutputStream" ,
                        "java.io.IOException",
                        "java.time.*",
                        "java.util.ArrayList",
                        "com.aiiot.distributed.api.TypeReader",
                        "com.aiiot.distributed.api.CommonData",
                        "com.aiiot.distributed.api.TypeWriter",
                        "com.aiiot.distributed.common.CommonStreamer",
                }),
                new String[]{ "TypeReader","TypeWriter"}, null);
        this.typeInfo = typeInfo;
        this.version = version;
    }


    private static String[] getImports(TypeInfo typeInfo, String[] strings) {
        HashSet<String> res=new HashSet<>();
        typeInfo.fields().stream().map(field -> field.fieldTypeInfo().listType()!=null?field.fieldTypeInfo().listType():field.fieldTypeInfo()).filter(fieldTypeInfo -> fieldTypeInfo.typeInfo()!=null && fieldTypeInfo.typeInfo()[0]!=null).map(fieldTypeInfo -> fieldTypeInfo.typeInfo()[0]).forEach(typeInfo1 -> res.add(typeInfo1.getPackageName()));
        ArrayList<String> res1=new ArrayList<>(Arrays.stream(strings).toList());
        for (String re : res) {
            res1.add(re+".*");
        }
        return res1.toArray(String[]::new);
    }


    public String getContent() {
        String content = """
                        public __className__() {
                        }
                    
                        @Override
                        public void write(DataOutputStream dataOutputStream, CommonData base) throws IOException {
                            __entityName__ commonData=(__entityName__)base;
                            CommonStreamer.write(dataOutputStream,commonData);
                            __writeFields__
                        }
                    
                        @Override
                        public CommonData read(int info, DataInputStream dataInputStream) throws IOException {
                            __entityName__ commonData=new __entityName__();
                            CommonStreamer.read(dataInputStream,info,commonData);
                            __readFields__
                            return commonData;
                        }
                        
                        public static __entityName__ create() {
                            __entityName__ commonData=new __entityName__();
                            commonData.version=__version__;
                            commonData.type=__type__;
                            return commonData;
                        }
                        
                    
                """;

        replaceMap = new LinkedHashMap<>();
        replaceMap.put("entityName",typeInfo.className());
        replaceMap.put("type",typeInfo.typeId()[0]+"");
        replaceMap.put("version",version+"");
        replaceMap.put("className",typeInfo.className()+"Stream");
        replaceMap.put("writeFields",getWriteField(typeInfo));
        replaceMap.put("readFields",getReadFields(typeInfo));
        return content;
    }

    private String getReadFields(TypeInfo typeInfo) {
        StringBuilder builder=new StringBuilder();
        boolean first=true;
        while (typeInfo!=null) {
            for (Field field : typeInfo.fields()) {
                if (!first)
                    builder.append("            ");
                first=false;
                builder.append(field.read());
            }

            typeInfo=typeInfo.parent()[0];
        }
        return builder.toString();
    }

    private String getWriteField(TypeInfo typeInfo) {
        StringBuilder builder=new StringBuilder();
        boolean first=true;
        while (typeInfo!=null) {
            for (Field field : typeInfo.fields()) {
                if (!first)
                    builder.append("            ");
                first=false;
                builder.append(field.write());
            }

            typeInfo=typeInfo.parent()[0];
        }
        return builder.toString();
    }

    @Override
    public Map<String, String> getValues() {
        return replaceMap;
    }
}
