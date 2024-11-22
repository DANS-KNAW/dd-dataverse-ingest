/*
 * Copyright (C) 2024 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.ingest.core;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_event")
public class TaskEvent {
    public enum EventType {
        ENQUEUE,
        START_PROCESSING,
        END_PROCESSING
    }

    public enum Result {
        OK,
        FAILED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "source")
    private String source;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "deposit_id", nullable = false, length = 36)
    private String depositId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String evenType;

    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "message", length = 1000)
    private String message;

    public TaskEvent() {
    }

    public TaskEvent(String source, OffsetDateTime timestamp, UUID depositId, EventType evenType, Result result, String message) {
        this.source = source;
        this.timestamp = timestamp;
        this.depositId = depositId.toString();
        this.evenType = evenType.name();
        this.result = result.name();
        this.message = StringUtils.truncate(message, 1000); // truncate to the max width of message
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getDepositId() {
        return UUID.fromString(depositId);
    }

    public void setDepositId(UUID depositId) {
        this.depositId = depositId.toString();
    }

    public EventType getEvenType() {
        return EventType.valueOf(evenType);
    }

    public void setEvenType(EventType evenType) {
        this.evenType = evenType.name();
    }

    public Result getResult() {
        return Result.valueOf(result);
    }

    public void setResult(Result result) {
        this.result = result.name();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = StringUtils.truncate(message, 1000);
    }
}
