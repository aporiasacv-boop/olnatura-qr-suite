import * as React from "react";
import { makeStyles, mergeClasses, shorthands } from "@fluentui/react-components";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  root: {
    backgroundColor: brand.surface,
    borderRadius: "12px",
    boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("16px"),
    transition: "box-shadow 0.2s ease",
  },
  clickable: {
    cursor: "pointer",
    ":hover": {
      boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
    },
  },
});

type AppCardProps = React.HTMLAttributes<HTMLDivElement> & {
  clickable?: boolean;
};

export default function AppCard({ clickable, className, ...props }: AppCardProps) {
  const s = useStyles();
  return (
    <div
      className={mergeClasses(s.root, clickable && s.clickable, className)}
      {...props}
    />
  );
}
