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

    private Pair<Envelope, Offset> convertEvent(Pair<TweetImageEvent, Offset> pair) {
        return new Pair<>(pair.first().getEnvelope(), pair.second());
    }

    public Topic<Envelope> tweetImageTopic() {
        return TopicProducer.singleStreamWithOffset(offset ->
                persistentEntityRegistry.eventStream(TweetImageEventTag.INSTANCE, offset)
                .map(this::convertEvent));
    }
}
