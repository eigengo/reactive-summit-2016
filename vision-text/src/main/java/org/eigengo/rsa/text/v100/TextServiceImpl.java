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
import akka.stream.javadsl.Flow;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import org.eigengo.rsa.Envelope;

public class TextServiceImpl implements TextService {
    private final PersistentEntityRegistry persistentEntityRegistry;

    @Inject
    public TextServiceImpl(PersistentEntityRegistry persistentEntityRegistry, TweetImageService tweetImageService) {
        this.persistentEntityRegistry = persistentEntityRegistry;
        tweetImageService.tweetImageTopic().subscribe().withGroupId("text").atLeastOnce(Flow.fromFunction(this::extractText));
    }

    private Done extractText(Envelope envelope) {
        System.out.println(envelope);
        return Done.getInstance();
    }

}
