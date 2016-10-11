package org.eigengo.rsa.text.v100;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import org.eigengo.rsa.Envelope;

interface E extends AggregateEvent<E> {
    Envelope getEnvelope();

    @Override
    default AggregateEventTag<E> aggregateTag() {
        return ETag.INSTANCE;
    }
}

class ETag {
    static AggregateEventTag<E> INSTANCE = AggregateEventTag.of(E.class, "in");
}
