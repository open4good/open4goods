Tu es en attente d'information (state=blocked).

Objectif:
- Vérifier si le dernier commentaire apporte les infos attendues.
- Si oui => next_state="interacting" (ou "done" si tout est bouclé).
- Si non => next_state="blocked" et reformule les questions de manière plus concrète.

Labels:
- Si tu restes en attente, conserve agent.blocked via labels_add (si nécessaire).

Règles:
- Réponds UNIQUEMENT en JSON conforme au schema fourni (aucun texte hors JSON).
