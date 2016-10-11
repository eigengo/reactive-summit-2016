package org.eigengo.rsa.text.v100;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import org.eigengo.rsa.Envelope;

import static com.lightbend.lagom.javadsl.api.Service.*;
import static com.lightbend.lagom.javadsl.api.Service.topic;

public interface TweetImageService extends Service {

    Topic<Envelope> tweetImageTopic();

    @Override
    default Descriptor descriptor() {
        return named("tweet-image")
                .publishing(
                        topic("tweet-image", this::tweetImageTopic).withMessageSerializer(ScalaPBMessageSerializer.of(Envelope.messageCompanion()))
                );
    }

}
