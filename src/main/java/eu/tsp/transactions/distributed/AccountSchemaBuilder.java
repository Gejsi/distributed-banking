package eu.tsp.transactions.distributed;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import eu.tsp.transactions.Account;

@AutoProtoSchemaBuilder(includeClasses = {
    Account.class }, schemaFileName = "account.proto", schemaFilePath = "proto/", schemaPackageName = "eu.tsp.transactions")
public interface AccountSchemaBuilder extends SerializationContextInitializer {
}
