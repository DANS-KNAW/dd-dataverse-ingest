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
package nl.knaw.dans.ingest.db;

import io.dropwizard.hibernate.AbstractDAO;
import nl.knaw.dans.ingest.core.TaskEvent;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.LinkedList;
import java.util.List;

public class TaskEventDAO extends AbstractDAO<TaskEvent> {

    public TaskEventDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public TaskEvent save(TaskEvent taskEvent) {
        return persist(taskEvent);
    }

    public List<TaskEvent> getEvents(String sourceName, String depositId) {
        // TODO: add filter criteria for eventType, result, max-age
        CriteriaBuilder cb = currentSession().getCriteriaBuilder();
        CriteriaQuery<TaskEvent> crit = cb.createQuery(TaskEvent.class);
        Root<TaskEvent> r = crit.from(TaskEvent.class);
        List<Predicate> predicates = new LinkedList<>();
        if (sourceName != null) {
            predicates.add(cb.equal(r.get("source"), sourceName));
        }
        if (depositId != null) {
            predicates.add(cb.equal(r.get("depositId"), depositId));
        }
        crit
            .select(r)
            .where(cb.and(predicates.toArray(new Predicate[0])))
            .orderBy(cb.asc(r.get("timestamp")));

        return currentSession().createQuery(crit).list();
    }


/*
convert to HQL to get "StateStats"

select event_type, count(*)
from task_event as t1
where t1.timestamp = (select max(timestamp)
                      from task_event as t2
                      where t2.deposit_id = t1.deposit_id and t2.source = 'test/batch'
                      group by t2.deposit_id)
group by event_type;
 */



    // TODO: figure this out
    //    public StateStats getStateStats(String sourceName) {
    //        CriteriaBuilder cb = currentSession().getCriteriaBuilder();
    //        CriteriaQuery<TaskEvent> crit = cb.createQuery(TaskEvent.class);
    //        Root<TaskEvent> r = crit.from(TaskEvent.class);
    //        List<Predicate> predicates = new LinkedList<>();
    //        if (sourceName != null) {
    //            predicates.add(cb.equal(r.get("source"), sourceName));
    //        }
    //        crit.select(r)
    //            .where(cb.and(predicates.toArray(new Predicate[0])))
    //            .groupBy(r.get("depositId"))
    //            .multiselect(r.get("event_type"), cb.max(r.get("timestamp")));
    //          currentSession().createQuery(crit).list();
    //
    //
    //    }

}
