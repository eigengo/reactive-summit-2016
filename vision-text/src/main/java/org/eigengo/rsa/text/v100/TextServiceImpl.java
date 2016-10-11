/*
 * The Reactive Summit Austin talk
 * Copyright (C) 2016 Jan Machacek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.eigengo.rsa.text.v100;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.*;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import org.eigengo.rsa.Envelope;

public class TextServiceImpl implements TextService {
    final Source<Envelope, SourceQueueWithComplete<Envelope>> source = Source.<Envelope>queue(10, OverflowStrategy.dropTail());
    final Sink<Envelope, SinkQueueWithCancel<Envelope>> queue = Sink.<Envelope>queue();
    final Flow<Envelope, Envelope, NotUsed> flow = Flow.fromSinkAndSource(Sink.<Envelope>ignore(), source).map(this::extractText);

    @Override
    public Topic<Envelope> textTopic() {
        return TopicProducer.<Envelope>singleStreamWithOffset(offset -> source.map(e -> new Pair<>(e, offset)));
    }

    @Inject
    public TextServiceImpl(TweetImageService tweetImageService) {
        tweetImageService.tweetImageTopic().subscribe().withGroupId("text").atLeastOnce(flow.map(x -> Done.getInstance()));
    }

    private Envelope extractText(Envelope envelope) {
        System.out.println("*** find text in " + envelope.messageId());

        return envelope;
    }

}
