package org.eigengo.rsa.text.v100;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import org.eigengo.rsa.Envelope;

import static com.lightbend.lagom.javadsl.api.Service.*;

public interface TextService extends Service {

    Topic<Envelope> textTopic();

    Topic<Envelope> tweetImageTopic();

    @Override
    default Descriptor descriptor() {
        return named("text")
                .publishing(topic("text", this::textTopic))
                .withMessageSerializer(Envelope.class, new ScalaPBMessageSerializer<>(Envelope.messageCompanion()));
    }

}
