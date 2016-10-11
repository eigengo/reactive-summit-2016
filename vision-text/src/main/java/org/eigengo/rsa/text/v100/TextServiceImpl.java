package org.eigengo.rsa.text.v100;

import akka.stream.scaladsl.Flow;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import org.eigengo.rsa.Envelope;

public class TextServiceImpl implements TextService {

    public TextServiceImpl() {
        tweetImageTopic()
                .subscribe()
                .atLeastOnce(Flow.fromFunction((Envelope x) -> { return extractText(x); }));
    }

    private Envelope extractText(Envelope envelope) {

    }

    @Override
    public Topic<Envelope> textTopic() {
        return null;
    }

    @Override
    public Topic<Envelope> tweetImageTopic() {
        return null;
    }
}
