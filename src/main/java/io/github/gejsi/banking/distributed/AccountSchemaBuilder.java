package io.github.gejsi.banking.distributed;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import io.github.gejsi.banking.Account;

@AutoProtoSchemaBuilder(includeClasses = {
    Account.class }, schemaFileName = "account.proto", schemaFilePath = "proto/", schemaPackageName = "io.github.gejsi.banking")
public interface AccountSchemaBuilder extends SerializationContextInitializer {
}
