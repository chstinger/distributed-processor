package com.aiiot.distributed.processor.writes;

import com.aiiot.distributed.processor.bean.ClassWriter;
import com.aiiot.distributed.processor.bean.TypeInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created : 30/07/2023-22.19
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public class TypeMapClass extends ClassWriter {


    private LinkedHashMap<String, String> replaceMap;
    private List<TypeInfo> typeInfoList;

    public TypeMapClass(String packageName, String name, List<TypeInfo> typeInfoList) {

        super(packageName, name, false, new String[]{
                        "java.io.DataInputStream" ,
                        "java.io.DataOutputStream" ,
                        "java.io.IOException",
                        "com.aiiot.distributed.api.TypeReader",
                        "com.aiiot.distributed.api.CommonData",
                        "com.aiiot.distributed.api.TypeWriter",
                        "com.aiiot.distributed.api.TypeMap",
                        "com.aiiot.distributed.common.CommonStreamer",
                },
                new String[]{ "TypeMap"}, null);
        this.typeInfoList = typeInfoList;
    }


    public String getContent() {
        String content = """
                        
                                            
                        private TypeReader[] typeReaders=new TypeReader[__size__];
                         
                        private TypeWriter[] typeWriters=new TypeWriter[__size__];

                        public __className__() {
                            __initTypeReaders__
                            __initTypeWriters__
                        }
                        
                        @Override
                        public TypeReader getTypeReader(int type){
                            return typeReaders[type];
                        }
                    
                        @Override
                        public TypeWriter getTypeWriter(int type) {
                            return typeWriters[type];
                        }
                    
                """;

        replaceMap = new LinkedHashMap<>();
        replaceMap.put("className",getName());
        replaceMap.put("size",typeInfoList.size()+"");

        replaceMap.put("initTypeReaders",getReaders(typeInfoList));
        replaceMap.put("initTypeWriters",getWriters(typeInfoList));
        return content;
    }

    private String getReaders(List<TypeInfo> typeInfoList) {
        StringBuilder builder=new StringBuilder();
        int index=0;
        for (TypeInfo typeInfo : typeInfoList) {
            if (typeInfo.isEnum())
                continue;
            if (index>0)
                builder.append("            ");
            builder.append("typeReaders[").append(index).append("]=new ").append(typeInfo.packageClassName()).append("Stream();\n");
            index++;
        }
        return builder.toString();
    }

    private String getWriters(List<TypeInfo> typeInfoList) {
        StringBuilder builder=new StringBuilder();
        int index=0;
        for (TypeInfo typeInfo : typeInfoList) {
            if (typeInfo.isEnum())
                continue;
            if (index>0)
                builder.append("            ");
            builder.append("typeWriters[").append(index).append("]=new ").append(typeInfo.packageClassName()).append("Stream();\n");
            index++;
        }
        return builder.toString();
    }


    @Override
    public Map<String, String> getValues() {
        return replaceMap;
    }
}
