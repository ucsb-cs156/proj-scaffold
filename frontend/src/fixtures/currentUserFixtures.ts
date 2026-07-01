export const apiCurrentUserFixtures = {
  userOnly: {
    user: { email: "cgaucho@ucsb.edu", given_name: "Gaucho" },
    roles: [{ authority: "ROLE_USER" }],
  },
  adminUser: {
    user: { email: "admin@ucsb.edu", given_name: "Admin" },
    roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_ADMIN" }],
  },
  instructorUser: {
    user: { email: "diba@ucsb.edu", given_name: "Diba" },
    roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_INSTRUCTOR" }],
  },
};

export const currentUserFixtures = {
  notLoggedIn: { loggedIn: false as const, root: null },
  userOnly: {
    loggedIn: true as const,
    root: {
      ...apiCurrentUserFixtures.userOnly,
      rolesList: ["ROLE_USER"],
    },
  },
  adminUser: {
    loggedIn: true as const,
    root: {
      ...apiCurrentUserFixtures.adminUser,
      rolesList: ["ROLE_USER", "ROLE_ADMIN"],
    },
  },
  instructorUser: {
    loggedIn: true as const,
    root: {
      ...apiCurrentUserFixtures.instructorUser,
      rolesList: ["ROLE_USER", "ROLE_INSTRUCTOR"],
    },
  },
};
