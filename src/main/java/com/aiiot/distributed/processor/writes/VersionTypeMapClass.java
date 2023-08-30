package com.aiiot.distributed.processor.writes;

import com.aiiot.distributed.processor.bean.ClassWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created : 30/07/2023-22.19
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
public class VersionTypeMapClass extends ClassWriter {


    private LinkedHashMap<String, String> replaceMap;

    private List<Integer> versions;

    public VersionTypeMapClass(String packageName, String name, List<Integer> versions) {

        super(packageName, name, false, new String[]{
                        "com.aiiot.distributed.common.VersionTypeMapFactory",
                        "com.aiiot.distributed.api.TypeMap",
                        "com.aiiot.distributed.api.VersionTypeMap",
                },
                new String[]{ "VersionTypeMap"}, null);
        this.versions = versions;
    }


    public String getContent() {
        String content = """
                        
                        static {{
                            VersionTypeMapFactory.setInstance(new __className__());   
                        }}                                                        
                                            
                        private TypeMap[] typeMaps=new TypeMap[__size__];

                        public __className__() {
                            __typeMaps__
                        }
                        
                        @Override
                        public TypeMap getTypeMap(int version){
                            return typeMaps[version-__offset__];
                        }
                    
                """;

        replaceMap = new LinkedHashMap<>();
        replaceMap.put("className",getName());
        replaceMap.put("size",versions.size()+"");
        replaceMap.put("offset","0"); // Todo calculated off

        replaceMap.put("typeMaps", getTypeMaps(versions));
        return content;
    }

    private String getTypeMaps(List<Integer> versions) {
        StringBuilder builder=new StringBuilder();
        int index=0;
        for (Integer version : versions) {
            if (index>0)
                builder.append("            ");
            builder.append("typeMaps[").append(index).append("]=new ").append("TypeMapVersion").append(version).append("();\n");
            index++;
        }
        return builder.toString();
    }


    @Override
    public Map<String, String> getValues() {
        return replaceMap;
    }
}
