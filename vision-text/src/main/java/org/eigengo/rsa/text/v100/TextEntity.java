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

import akka.NotUsed;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.protobuf.ByteString;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import org.eigengo.rsa.Envelope;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;
import java.util.UUID;

class TextEntity extends PersistentEntity<TextEntityCommand, TextEntityEvent, NotUsed> {

    @Override
    public Behavior initialBehavior(Optional<NotUsed> snapshotState) {
        BehaviorBuilder b = newBehaviorBuilder(null);
        b.setCommandHandler(TextEntityCommand.Ocr.class, (cmd, ctx) -> {
                    TextEntityEvent.Ocred event = new TextEntityEvent.Ocred(entityId(), cmd.correlationId, cmd.ingestionTimestamp, System.nanoTime(), "Text");
                    return ctx.thenPersist(event, e -> ctx.reply(NotUsed.getInstance()));
                }
        );
        b.setEventHandler(TextEntityEvent.Ocred.class, e -> NotUsed.getInstance());
        return b.build();
    }
}

interface TextEntityEvent extends Jsonable {
    class OcredTag {
        static AggregateEventTag<Ocred> INSTANCE = AggregateEventTag.of(Ocred.class, "text");
    }

    @Immutable
    @JsonDeserialize
    class Ocred implements TextEntityEvent, CompressedJsonable, AggregateEvent<Ocred> {
        final String handle;
        final String correlationId;
        final long ingestionTimestamp;
        final long processingTimestamp;
        final String text;

        @JsonCreator
        public Ocred(String handle, String correlationId, long ingestionTimestamp, long processingTimestamp, String text) {
            this.handle = handle;
            this.correlationId = correlationId;
            this.ingestionTimestamp = ingestionTimestamp;
            this.processingTimestamp = processingTimestamp;
            this.text = text;
        }

        Envelope envelope() {
            System.out.println("*** making envelope");
            return Envelope.apply(100, this.processingTimestamp, this.ingestionTimestamp, this.handle, this.correlationId,
                    UUID.randomUUID().toString(), "text", ByteString.copyFrom(this.text, Charsets.UTF_8));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ocred ocred = (Ocred) o;
            return ingestionTimestamp == ocred.ingestionTimestamp &&
                    processingTimestamp == ocred.processingTimestamp &&
                    Objects.equal(correlationId, ocred.correlationId) &&
                    Objects.equal(handle, ocred.handle) &&
                    Objects.equal(text, ocred.text);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(correlationId, handle, ingestionTimestamp, processingTimestamp, text);
        }

        @Override
        public AggregateEventTag<Ocred> aggregateTag() {
            return OcredTag.INSTANCE;
        }
    }

}

interface TextEntityCommand extends Jsonable {

    @Immutable
    @JsonDeserialize
    class Ocr implements TextEntityCommand, CompressedJsonable, PersistentEntity.ReplyType<NotUsed> {
        final String correlationId;
        final long ingestionTimestamp;
        final byte[] image;

        @JsonCreator
        public Ocr(String correlationId, long ingestionTimestamp, byte[] image) {
            this.correlationId = correlationId;
            this.ingestionTimestamp = ingestionTimestamp;
            this.image = image;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ocr ocr = (Ocr) o;
            return ingestionTimestamp == ocr.ingestionTimestamp &&
                    Objects.equal(correlationId, ocr.correlationId) &&
                    Objects.equal(image, ocr.image);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(correlationId, ingestionTimestamp, image);
        }
    }

}
