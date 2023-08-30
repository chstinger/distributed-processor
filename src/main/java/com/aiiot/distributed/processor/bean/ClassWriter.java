package com.aiiot.distributed.processor.bean;

import lombok.Data;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created : 30/07/2023-21.44
 *
 * @author Claes Hougaard <cch@neoconsult.dk>
 */
@Data
public class ClassWriter {

    private String name;
    private String packageName;
    private boolean abstractClass;

    private List<String> implementsList=new ArrayList<>();
    private List<String> importsList=new ArrayList<>();
    private String extend="";

    public ClassWriter(String packageName, String name) {
        this.name = name;
        this.packageName = packageName;
    }

    public ClassWriter(String packageName, String name, boolean abstractClass, String[] importsList, String[] implementsList , String extend) {
        this.name = name;
        this.packageName = packageName;
        this.abstractClass = abstractClass;
        if (implementsList!=null)
            this.implementsList =  Arrays.asList(implementsList);
        if (importsList!=null)
            this.importsList = Arrays.asList(importsList);
        this.extend = extend;
    }

    public void write(ProcessingEnvironment processingEnv) {
        Filer filer = processingEnv.getFiler();
        JavaFileObject sourceFile = null;
        try {
            sourceFile = filer.createSourceFile(packageName +"."+name);
        } catch (IOException e) {
            System.out.println("DistributionAnnotationProcessor.process FILE ALREADY exists");
        }
        sourceFile.delete();
        try (Writer writer = sourceFile.openWriter()) {
            write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void write(Writer writer) throws IOException {
        StringBuilder builder=new StringBuilder();
        
        builder.append(String.format("package %1$s;\n", packageName));
        builder.append(getImports());
        if (abstractClass)
            builder.append(String.format("public abstract class %1$s %2$s%3$s{\n",name,getExtends(),getImplements()));
        else
            builder.append(String.format("public class %1$s %2$s%3$s{\n",name,getExtends(),getImplements()));

        builder.append(getContent());
        builder.append("}\n");
        writer.write(replaceValues(builder.toString(),getValues()));

    }

    private String replaceValues(String string, Map<String, String> values) {

        for (Map.Entry<String, String> entry : values.entrySet()) {
            string=string.replaceAll("¤¤"+entry.getKey()+"¤¤",entry.getValue());
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            string=string.replaceAll("__"+entry.getKey()+"__",entry.getValue());
        }
        return string;
    }

    public String getContent() {
        return "";
    }

    public Map<String,String> getValues () {
        return new HashMap<>();
    }

    private String getImplements() {
        if (implementsList.isEmpty())
            return "";
        return "implements "+ String.join(",", implementsList)+" ";
    }

    private String getExtends() {
        if (extend==null || extend.isBlank())
            return "";
        return "extends "+extend+" ";
    }

    private String getImports() {
        if (importsList.isEmpty())
            return "\n";
        return importsList.stream().map(s -> "import "+s+";").collect(Collectors.joining("\n"))+"\n";

    }

}
