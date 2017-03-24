package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;

/**
 * @author Edmund Kemsley This class holds MatsimAgent objects and information
 *         related to the agents in the Matsim system such the replanner
 *         MatsimAgents and extension of Matsim Agents are stored in
 *         matSimAgents Hashmap
 */
final class MATSimAgentManager {
	private AgentStateList agentStateList;
	private HashMap<Id<Person>, MATSimAgent> matSimAgents;
	private MATSimModel matSimModel;
	private AgentDataContainer agentDataContainer;
	private Replanner agentReplanner;

	MATSimAgentManager(MATSimModel model) {
		this.matSimModel = model;

		matSimAgents = new HashMap<Id<Person>, MATSimAgent>();
		agentStateList = new AgentStateList();
		this.agentDataContainer = new AgentDataContainer();
	}

	final void setUpReplanner(Replanner replanner, ActivityEndRescheduler activityEndRescheduler) {
		// Use default replanner if none provided
		if (replanner == null) {
			replanner = new Replanner(matSimModel, activityEndRescheduler);
		}
		this.agentReplanner = replanner;
	}

	final Replanner getReplanner() {
		return agentReplanner;
	}

	final AgentDataContainer getAgentDataContainer() {
		return agentDataContainer;
	}

	final AgentStateList getAgentStateList() {
		return agentStateList;
	}

	final MATSimAgent getAgent(Id<Person> agentID) {
		return matSimAgents.get(agentID);
	}

	final MATSimAgent getAgent(String agentID) {
		return matSimAgents.get(Id.createPersonId(agentID));
	}

	/*
	 * Creates a new matsimAgent and it to the list Passes the ActionHandler,
	 * PerceptHandler, ActionContainer and PerceptContainer to matSimAgent
	 * Override this method and change the four parameters above to change
	 * functionality
	 */
	final boolean createAndAddBDIAgent(Id<Person> agentID) {
		ActionPerceptContainer agentContainer = agentDataContainer
				.getOrCreate(agentID.toString());
		MATSimAgent agent = new MATSimAgent(
				new MATSimActionHandler(matSimModel), 
				new MATSimPerceptHandler(matSimModel), 
				agentID,
				agentContainer.getActionContainer(),
				agentContainer.getPerceptContainer());
		matSimAgents.put(agentID, agent);
		agentStateList.add(new AgentState(agentID.toString()));
		return true;
	}

	/**
	 * Register any new BDI actions and percepts provided by the application
	 * @param app
	 */
	final void registerApplicationActionsPercepts(MATSimApplicationInterface app) {
		for(Id<Person> agentId: matSimModel.getBDIAgentIDs()) {
			MATSimAgent agent = matSimModel.getBDIAgent(agentId);
			app.registerNewBDIActions(agent.getActionHandler());
			app.registerNewBDIPercepts(agent.getPerceptHandler());
		}
	}

	final boolean removeAgent(Id<Person> agentID) {
		matSimAgents.remove(agentID);
		agentStateList.remove(agentStateList.remove(new AgentState(agentID
				.toString())));// maybe will work
		// don't you need to also remove this from agentDataContainer??
		return true;
	}

	/*
	 * Called by MatsimModel to signal news actions from BDI side Handles two
	 * types of changes, new actions (INITIATED) and dropped actions
	 */
	final void updateActions(AgentDataContainer agentDataContainer) {
		if (agentDataContainer.isEmpty()) {
			return;
		}
		for (String agent : agentDataContainer.keySet()) {
			ActionContainer actionContainer = agentDataContainer.getOrCreate(
					agent).getActionContainer();
			for (String action : actionContainer.actionIDSet()) {
				if (actionContainer.get(action).getState() == ActionContent.State.INITIATED) {
					initiateNewAction(agent, action);
					/*
					 * dsingh; 21/apr/15: remove RUNNING actions for efficiency
					 * reasons so that we do not transit them back and forth.
					 * When the action is finished we will add it again with the
					 * new status.
					 */
					actionContainer.remove(action);
				} else if (actionContainer.get(action).getState() == ActionContent.State.DROPPED) {
					dropAction(agent, action);
				}
			}
		}
	}

	/*
	 * BDI side passed an action with state INITIATED Pass action parameters to
	 * ActionHandler then update action to RUNNING
	 */
	private final boolean initiateNewAction(String agentID, String actionID) {
		// if (matSimAgents.containsKey(new IdImpl(agentID))){
		if (matSimAgents.containsKey(Id.createPersonId(agentID))) {
			MATSimAgent agent = getAgent(agentID);
			Object[] parameters = agent.getActionContainer().get(actionID)
					.getParameters();
			if (agent.getActionHandler().processAction(agentID, actionID,
					parameters)) {
				agent.getActionContainer().get(actionID.toString())
						.setState(ActionContent.State.RUNNING);
				return true;
			} else {
				agent.getActionContainer().get(actionID.toString())
						.setState(ActionContent.State.FAILED);
				return false;
			}
		}
		return false;
	}

	/*
	 * BDI side wants to drop an action
	 */
	private final void dropAction(String agentID, String actionID) {
		// if (matSimAgents.containsKey(new IdImpl(agentID))){
		if (matSimAgents.containsKey(Id.createPersonId(agentID))) {

		}
	}
}