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
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import org.eigengo.rsa.Envelope;

public class TextServiceImpl implements TextService {
    private final Source<Envelope, SourceQueueWithComplete<Envelope>> queueSource = Source.<Envelope>queue(10, OverflowStrategy.backpressure());
    private final SourceQueueWithComplete<Envelope> queue;

    @Override
    public Topic<Envelope> textTopic() {
        Source<Envelope, ?> src = queueSource; //Source.tick(FiniteDuration.Zero(), FiniteDuration.apply(1000, TimeUnit.MILLISECONDS), Envelope.defaultInstance());

        return TopicProducer.singleStreamWithOffset(offset -> src.map(e -> new Pair<>(e, offset)));
    }

    @Inject
    public TextServiceImpl(Materializer materializer, TweetImageService tweetImageService) {
        this.queue = queueSource.to(Sink.ignore()).run(materializer);
        tweetImageService.tweetImageTopic().subscribe().withGroupId("text").atLeastOnce(Flow.fromFunction(this::extractText));
    }

    private Done extractText(Envelope envelope) {
        System.out.println("*** find text in " + envelope.messageId());
        queue.offer(envelope);
        return Done.getInstance();
    }

}
