// The import rules of web/src/, which `pnpm check:imports` holds. The code
// sits in five layers, from the top: app, pages, features, entities and
// shared. pages, features and entities are divided into slices, a folder
// each; app and shared are not.

// Fails the cruise when the parser can't load: without it,
// dependency-cruiser reads no module, finds nothing to refuse and exits 0.
require("@swc/core");

const onlyBelow = (name, above) => ({
	name: `${name}-imports-only-below`,
	comment: `${name} imports only the layers below it.`,
	severity: "error",
	from: { path: `^src/${name}/` },
	to: { path: `^src/(${above})/` },
});

// A file inside a slice of the layer is left to slices-never-each-other:
// the slice it reaches is its own, which it reaches into freely, or
// another, which it never imports. A file that sits in the layer and in no
// slice is outside every slice.
const throughIndex = (name) => ({
	name: `${name}-through-their-index`,
	comment: `Code outside a slice of ${name} imports it through its index.ts.`,
	severity: "error",
	from: { pathNot: `^src/${name}/[^/]+/` },
	to: {
		path: `^src/${name}/[^/]+/`,
		pathNot: `^src/${name}/[^/]+/index\\.ts$`,
	},
});

module.exports = {
	forbidden: [
		// app is the top layer, so no import of its own breaks the order.
		onlyBelow("pages", "app"),
		onlyBelow("features", "app|pages"),
		onlyBelow("entities", "app|pages|features"),
		onlyBelow("shared", "app|pages|features|entities"),
		{
			name: "slices-never-each-other",
			comment: "A slice never imports another slice on its own layer.",
			severity: "error",
			from: { path: "^src/(pages|features|entities)/([^/]+)/" },
			to: { path: "^src/$1/", pathNot: "^src/$1/$2/" },
		},
		throughIndex("pages"),
		throughIndex("features"),
		throughIndex("entities"),
		// An import that resolves to no file passes every rule above, since
		// each matches the path of the file an import reaches.
		{
			name: "every-import-resolves",
			comment: "Every import resolves to a file or a package.",
			severity: "error",
			from: {},
			to: { couldNotResolve: true },
		},
	],
	options: {
		// The pinned TypeScript 7.0 lacks the compiler API dependency-cruiser
		// parses TypeScript through.
		parser: "swc",
		// `@/` resolves as `paths` in tsconfig.json has it.
		tsConfig: { fileName: "tsconfig.json" },
		doNotFollow: { path: "node_modules" },
	},
};
