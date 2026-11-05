import { compose } from "./stack";

export default function globalTeardown(): void {
	compose("down", "--volumes");
}
