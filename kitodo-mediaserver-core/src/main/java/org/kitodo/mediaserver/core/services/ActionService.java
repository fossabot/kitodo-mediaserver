/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * LICENSE file that was distributed with this source code.
 */

package org.kitodo.mediaserver.core.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.kitodo.mediaserver.core.api.IAction;
import org.kitodo.mediaserver.core.db.entities.ActionData;
import org.kitodo.mediaserver.core.db.entities.Work;
import org.kitodo.mediaserver.core.db.repositories.ActionRepository;
import org.kitodo.mediaserver.core.exceptions.ActionServiceException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Run Actions.
 */
@Service
public class ActionService {

    private ApplicationContext applicationContext;

    private ActionRepository actionRepository;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setActionRepository(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    private ActionService() {}

    /**
     * Request a action and make it persistent.
     * @param work the Work
     * @param actionName action bean name to be used
     * @param parameter parameter list
     * @return ActionData entry
     */
    public ActionData request(Work work, String actionName, Map<String, String> parameter) {

        // if this exact action is already requested and not finished, use it
        ActionData actionData = getUnfinishedAction(work, actionName, parameter);
        if (actionData != null) {
            return actionData;
        }

        // if this action doesn't exist create a new one
        actionData = new ActionData(work, actionName, parameter);
        actionData.setRequestTime(Instant.now());
        actionRepository.save(actionData);
        return actionData;
    }

    /**
     * Run a saved ActionData.
     * @param actionData ActionData to run
     * @return the result of the Action
     * @throws Exception on Action errors
     */
    public Object performRequested(ActionData actionData) throws Exception {

        Object actionInstance;

        // Does the action bean exist?
        try {
            actionInstance = applicationContext.getBean(actionData.getActionName());
        } catch (BeansException ex) {
            throw new ClassNotFoundException("No Bean found for actionName='" + actionData.getActionName() + "'");
        }

        // Is the bean a usable Action?
        if (!(actionInstance instanceof IAction)) {
            throw new ActionServiceException("Saved actionName='" + actionData.getActionName()
                + "' for workId=" + actionData.getWork().getId() + " is not a valid action bean.");
        }

        // Is this action already running?
        ActionData persistentActionData = getRunningAction(actionData.getWork(), actionData.getActionName(), actionData.getParameter());
        if (persistentActionData != null) {
            throw new ActionServiceException("This actionName='" + actionData.getActionName()
                + "' for workId=" + actionData.getWork().getId() + " is already running: startTime=" + persistentActionData.getStartTime());
        }

        // Run the action...
        actionData.setStartTime(Instant.now());
        actionRepository.save(actionData);

        Object result = ((IAction)actionInstance).perform(actionData.getWork(), actionData.getParameter());

        actionData.setEndTime(Instant.now());
        actionRepository.save(actionData);

        return result;
    }

    /**
     * Finds the last finished action.
     * @param work the Work
     * @param actionName the action bean name
     * @return the ActionData
     */
    public ActionData getLastFinishedAction(Work work, String actionName) {
        List<ActionData> actionDatas = actionRepository.findByWorkAndActionNameOrderByEndTimeDesc(work, actionName);
        if (actionDatas != null && actionDatas.size() > 0) {
            return actionDatas.get(0);
        }
        return null;
    }

    /**
     * Finds the unfinished action (not running and running).
     * @param work the Work
     * @param actionName the action bean name
     * @param parameter the parameter list
     * @return the ActionData
     */
    public ActionData getUnfinishedAction(Work work, String actionName, Map<String, String> parameter) {
        List<ActionData> actionDatas = actionRepository.findByWorkAndActionNameAndEndTimeIsNull(work, actionName);
        return getMatchingAction(actionDatas, parameter);
    }

    /**
     * Finds the currently running action.
     * @param work the Work
     * @param actionName the action bean name
     * @param parameter the parameter list
     * @return the ActionData
     */
    public ActionData getRunningAction(Work work, String actionName, Map<String, String> parameter) {
        List<ActionData> actionDatas = actionRepository.findByWorkAndActionNameAndStartTimeIsNotNullAndEndTimeIsNull(work, actionName);
        return getMatchingAction(actionDatas, parameter);
    }

    /**
     * Filters a ActionData list for a matching parameter list.
     * @param actionDatas ActionData list
     * @param parameter the parameter list
     * @return the matching ActionData
     */
    private ActionData getMatchingAction(List<ActionData> actionDatas, Map<String, String> parameter) {
        for (ActionData actionData : actionDatas) {
            if (parameter.equals(actionData.getParameter())) {
                return actionData;
            }
        }
        return null;
    }

}
