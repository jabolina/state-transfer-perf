package org.infinispan.data;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.protostream.annotations.Proto;

@Proto
@Indexed(keyEntity = "test.PersonKey")
public record Person(
      @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false)
      String firstName,

      @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false)
      String lastName,
      int bornYear,

      @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false)
      String bornIn
) {
   public static Person create() {
      String content = UUID.randomUUID().toString();
      return new Person(content, content, ThreadLocalRandom.current().nextInt(), content.repeat(3));
   }
}
