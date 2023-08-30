package com.aiiot.distributed.processor;

import com.aiiot.distributed.api.*;
import com.aiiot.distributed.api.clazz.Entity;
import com.aiiot.distributed.api.clazz.Change;
import com.aiiot.distributed.api.clazz.Query;
import com.aiiot.distributed.api.clazz.Reply;
import com.aiiot.distributed.processor.bean.ChangeQueryInfo;
import com.aiiot.distributed.processor.bean.DistributedInfo;
import com.aiiot.distributed.processor.bean.TypeInfo;
import com.aiiot.distributed.processor.writes.EntityHandlerClass;
import com.aiiot.distributed.processor.writes.MessageStreamerClass;
import com.aiiot.distributed.processor.writes.TypeMapClass;
import com.aiiot.distributed.processor.writes.VersionTypeMapClass;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.*;

@SupportedAnnotationTypes({
        "com.aiiot.distributed.api.Version",
        "com.aiiot.distributed.api.clazz.Entity",
        "com.aiiot.distributed.api.clazz.EntityChange",
        "com.aiiot.distributed.api.clazz.EntityChangeHandler",
        "com.aiiot.distributed.api.clazz.EntityChangeReply",
        "com.aiiot.distributed.api.field.Index",
        "com.aiiot.distributed.api.field.Mandatory",
        "com.aiiot.distributed.api.field.Key",
        "com.aiiot.distributed.api.field.Optional",
        "com.aiiot.distributed.api.SubEntity",
        "com.aiiot.distributed.api.Base",
        "com.aiiot.distributed.api.EntryPoint",
        "com.aiiot.distributed.services.DistributedStreamingService",


})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DistributionAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("DistributionAnnotationProcessor.process "+annotations);
        if (annotations.isEmpty())
            return false;


        DistributedInfo distributedInfo=new DistributedInfo(roundEnv.getElementsAnnotatedWith(EntryPoint.class));

        distributedInfo.setBaseInfos(Common.getTypeInfos(roundEnv.getElementsAnnotatedWithAny(Set.of(Entity.class, Reply.class,Change.class,Query.class))));

        if (!distributedInfo.validateBaseInfo())
            throw new RuntimeException("Types info validation error");
        

        distributedInfo.setChangeInfos(Common.getChangeQueryInfo(distributedInfo,roundEnv.getElementsAnnotatedWith(Change.class)));
        distributedInfo.setQueryInfos(Common.getChangeQueryInfo(distributedInfo,roundEnv.getElementsAnnotatedWith(Query.class)));

        distributedInfo.setReplyInfos(Common.getReplyInfo(distributedInfo,roundEnv.getElementsAnnotatedWith(Reply.class)));



        int currentVersion = distributedInfo.getCurrentVersion(); // Todo get versions
        //System.out.println("DistributionAnnotationProcessor.process currentVersion "+currentVersion);
        List<TypeInfo> versionTypeMap = distributedInfo.getVersionTypeMap(currentVersion);
        //System.out.println("DistributionAnnotationProcessor.process versionInfo "+versionTypeMap);
        distributedInfo.getBaseInfos().forEach((s, commonEntityInfo) -> {
            if (commonEntityInfo.isEnum())
                return;
            MessageStreamerClass messageStreamerClass=new MessageStreamerClass(commonEntityInfo,currentVersion);
            messageStreamerClass.write(processingEnv);
        });



        TypeMapClass typeMapClass =new TypeMapClass(distributedInfo.getPackageName(), "TypeMapVersion"+ currentVersion, versionTypeMap);
        typeMapClass.write(processingEnv);

        VersionTypeMapClass versionTypeMapClass=new VersionTypeMapClass(distributedInfo.getPackageName(),"TypeMapVersionAll",distributedInfo.getVersions());
        versionTypeMapClass.write(processingEnv);

        //Write Entity  Handler
        ArrayList<ChangeQueryInfo> changeQueryInfos = new ArrayList<>(distributedInfo.getChangeInfos().values());
        changeQueryInfos.addAll(distributedInfo.getQueryInfos().values());

        HashMap<String,TypeInfo> entityMap=new HashMap<>();

        for (ChangeQueryInfo changeQueryInfo : changeQueryInfos) {
            TypeInfo typeInfo=distributedInfo.getTypeInfo(changeQueryInfo.entityName());
            if (typeInfo!=null) {
                entityMap.put(typeInfo.packageClassName(),typeInfo);
            }

        }
        if (!changeQueryInfos.isEmpty()) {
            EntityHandlerClass entityHandlerClass = new EntityHandlerClass(distributedInfo.getPackageName(), "SingleEntityHandler",entityMap, changeQueryInfos,distributedInfo);
            entityHandlerClass.write(processingEnv);
        }

        //Todo Write load balancer handler
        

        //Todo Get key from entity
        


        return true;
    }
}