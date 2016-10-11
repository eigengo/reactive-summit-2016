package org.eigengo.rsa.text.v100;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.eigengo.rsa.Envelope;

import java.util.Optional;

interface TextEvent extends AggregateEvent<TextEvent> {
    Envelope getEnvelope();

    @Override
    default AggregateEventTag<TextEvent> aggregateTag() {
        return TextEventTag.INSTANCE;
    }
}

class TextEventTag {
    static AggregateEventTag<TextEvent> INSTANCE = AggregateEventTag.of(TextEvent.class, "text");
}

class TextEntity extends PersistentEntity<Envelope, TextEvent, NotUsed> {

    @Override
    public Behavior initialBehavior(Optional<NotUsed> snapshotState) {
        return null;
    }
}