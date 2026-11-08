import { compose } from "./stack";

// A fresh database on every run: a volume left by a run that stopped
// before its teardown goes first.
export default function globalSetup(): void {
	compose("down", "--volumes");
	compose("up", "--wait");
}
