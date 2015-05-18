/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.core.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.prop4j.Literal;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.conf.nodes.Expression;
import de.ovgu.featureide.fm.core.conf.nodes.Variable;
import de.ovgu.featureide.fm.core.conf.nodes.VariableConfiguration;
import de.ovgu.featureide.fm.core.conf.worker.CalcThread2;
import de.ovgu.featureide.fm.core.conf.worker.ISatThread;
import de.ovgu.featureide.fm.core.conf.worker.base.AWorkerThread;
import de.ovgu.featureide.fm.core.conf.worker.base.ThreadPool;
import de.ovgu.featureide.fm.core.editing.NodeCreator;

/**
 * TODO description
 * 
 * @author Sebastian Krieter
 */
public class ConfChanger implements IConfigurationChanger {
	private final FeatureGraph featureGraph;
	private final VariableConfiguration variableConfiguration;
	private final ThreadPool<Integer> dfsThread;

	private final List<Node> knownLiterals = new ArrayList<>();

	public ConfChanger(FeatureModel featureModel, FeatureGraph featureGraph, VariableConfiguration variableConfiguration) {
		this.featureGraph = featureGraph;
		this.variableConfiguration = variableConfiguration;
		this.dfsThread = new ThreadPool<>(new CalcThread2(featureGraph, this, NodeCreator.createNodes(featureModel, true).toCNF()));
		dfsThread.reset();
	}

	private int[] count = new int[4];

	private final LinkedList<String> changedFeatures = new LinkedList<>();

	public List<String> setFeature(Feature f, byte newValue) {
		if (newValue == Variable.UNDEFINED) {
			return Collections.emptyList();
		}
		changedFeatures.clear();

		final int index = featureGraph.getFeatureIndex(f.getName());
		set(index, newValue == Variable.TRUE);

		count = new int[4];

		List<Integer> compList = new ArrayList<>();

		if (newValue == Variable.TRUE) {
			for (int i = 0; i < featureGraph.getSize(); i++) {
				if (variableConfiguration.getVariable(i).getValue() != Variable.UNDEFINED) {
					count[0]++;
					continue;
				}
				final byte edge = (byte) (featureGraph.getEdge(index, i) & FeatureGraph.MASK_1_00001100);
				switch (edge) {
				case FeatureGraph.EDGE_10:
					set(i, false);
					break;
				case FeatureGraph.EDGE_11:
					set(i, true);
					break;
				case FeatureGraph.EDGE_1q:
					compList.add(i);
					continue;
				default:
					continue;
				}
			}
		} else {
			for (int i = 0; i < featureGraph.getSize(); i++) {
				if (variableConfiguration.getVariable(i).getValue() != Variable.UNDEFINED) {
					continue;
				}
				final byte edge = (byte) (featureGraph.getEdge(index, i) & FeatureGraph.MASK_0_00110000);
				switch (edge) {
				case FeatureGraph.EDGE_00:
					set(i, false);
					break;
				case FeatureGraph.EDGE_01:
					set(i, true);
					break;
				case FeatureGraph.EDGE_0q:
					compList.add(i);
					continue;
				default:
					continue;
				}
			}
		}

		for (Iterator<Integer> it = compList.iterator(); it.hasNext();) {
			final int i = it.next();
			final LinkedList<Expression> varExpList = featureGraph.getExpListAr().get(i);
			if (varExpList == null || varExpList.isEmpty()) {
				continue;
			}
			for (Expression expression : varExpList) {
				variableConfiguration.setVariable(i, Variable.TRUE);
				expression.updateValue();
				if (expression.getValue() == Variable.FALSE) {
					variableConfiguration.setVariable(i, Variable.UNDEFINED);
					set(i, false);
					it.remove();
					break;
				} else {
					variableConfiguration.setVariable(i, Variable.FALSE);
					expression.updateValue();
					if (expression.getValue() == Variable.FALSE) {
						variableConfiguration.setVariable(i, Variable.UNDEFINED);
						set(i, true);
						it.remove();
						break;
					} else {
						variableConfiguration.setVariable(i, Variable.UNDEFINED);
					}
				}
			}
		}

		//		count[2] = compList.size();
		//		count[3] = featureGraph.getSize() - (count[0] + count[1] + count[2]);
		//		System.out.println();
		//		System.out.println("Known:      " + count[0]);
		//		System.out.println("Unaffected: " + count[3]);
		//		System.out.println("Non Sat:    " + count[1]);
		//		System.out.println("Sat:        " + count[2]);

		knownLiterals.clear();

		int i = 0;
		for (Variable var : variableConfiguration) {
			switch (var.getValue()) {
			case Variable.TRUE:
				knownLiterals.add(new Literal(featureGraph.featureArray[i], true));
				break;
			case Variable.FALSE:
				knownLiterals.add(new Literal(featureGraph.featureArray[i], false));
				break;
			default:
				break;
			}
			i++;
		}

		dfsThread.reset();
		for (AWorkerThread<Integer> thread : dfsThread.getThreads()) {
			((ISatThread) thread).setKnownLiterals(knownLiterals);
		}
		dfsThread.addObjects(compList);
		dfsThread.run();

		Collections.sort(changedFeatures);
		return changedFeatures;
	}

	public void set(int index, boolean value) {
		if (variableConfiguration.getVariable(index).getValue() == Variable.UNDEFINED) {
			variableConfiguration.setVariable(index, value ? Variable.TRUE : Variable.FALSE);
			count[1]++;
			changedFeatures.add(featureGraph.featureArray[index]);
		}
	}

}
