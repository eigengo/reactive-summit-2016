package org.eigengo.rsa.text.v100;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import org.eigengo.rsa.Envelope;

interface TweetImageEvent extends AggregateEvent<TweetImageEvent> {
    Envelope getEnvelope();

    @Override
    default AggregateEventTag<TweetImageEvent> aggregateTag() {
        return TweetImageEventTag.INSTANCE;
    }
}

class TweetImageEventTag {
    static AggregateEventTag<TweetImageEvent> INSTANCE = AggregateEventTag.of(TweetImageEvent.class, "in");
}
