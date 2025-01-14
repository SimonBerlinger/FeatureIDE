/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.analysis.cnf.solver;

import java.util.List;

import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;

import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;

/**
 * Modified variable order for {@link ISolver}.<br> Uses the {@link UniformRandomSelectionStrategy}.
 *
 * @author Sebastian Krieter
 */
public class VarOrderHeap3 extends VarOrderHeap {

	private static final long serialVersionUID = 1L;

	private final UniformRandomSelectionStrategy selectionStrategy;

	public VarOrderHeap3(List<LiteralSet> sample) {
		super(new UniformRandomSelectionStrategy(sample));
		selectionStrategy = (UniformRandomSelectionStrategy) phaseStrategy;
	}

	@Override
	public void undo(int x) {
		super.undo(x);
		selectionStrategy.undo(x);
	}

	@Override
	public void assignLiteral(int p) {
		super.assignLiteral(p);
	}

}
