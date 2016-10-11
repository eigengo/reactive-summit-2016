package org.eigengo.rsa.text.v100;

import akka.japi.Pair;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.Offset;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import org.eigengo.rsa.Envelope;

public class TweetImageServiceImpl implements TweetImageService {
    private final PersistentEntityRegistry persistentEntityRegistry;

    @Inject
    public TweetImageServiceImpl(PersistentEntityRegistry persistentEntityRegistry) {
        this.persistentEntityRegistry = persistentEntityRegistry;
    }

    private Pair<Envelope, Offset> convertEvent(Pair<E, Offset> pair) {
        return new Pair<>(pair.first().getEnvelope(), pair.second());
    }

    public Topic<Envelope> tweetImageTopic() {
        return TopicProducer.singleStreamWithOffset(offset -> persistentEntityRegistry
                .eventStream(ETag.INSTANCE, offset)
                .map(this::convertEvent));
    }
}
