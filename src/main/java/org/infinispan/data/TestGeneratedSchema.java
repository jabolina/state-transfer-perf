package org.infinispan.data;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
        schemaPackageName = "test",
        includeClasses = {
              Person.class,
              PersonKey.class,
              HumongousEntry.class
        }
)
public interface TestGeneratedSchema extends GeneratedSchema { }
