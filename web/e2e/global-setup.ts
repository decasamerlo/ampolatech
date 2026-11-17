import { compose, stopIfAnotherProjectHoldsThePorts } from "./stack";

// A fresh database on every run: a volume left by a run that stopped
// before its teardown goes first. The ports are checked before `up`, which
// would build both images before it failed on a held port.
export default function globalSetup(): void {
	compose("down", "--volumes");
	stopIfAnotherProjectHoldsThePorts();
	compose("up", "--wait");
}
