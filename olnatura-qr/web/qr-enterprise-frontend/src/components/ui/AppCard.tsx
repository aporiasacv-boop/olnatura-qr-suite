import * as React from "react";
import { makeStyles, mergeClasses, shorthands } from "@fluentui/react-components";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  root: {
    backgroundColor: brand.surfaceMuted,
    backdropFilter: "blur(6px)",
    borderRadius: "12px",
    boxShadow: "0 1px 3px rgba(74, 92, 40, 0.05)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("16px"),
    transition: "box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease",
  },
  clickable: {
    cursor: "pointer",
  },
});

type AppCardProps = React.HTMLAttributes<HTMLDivElement> & {
  clickable?: boolean;
};

export default function AppCard({ clickable, className, ...props }: AppCardProps) {
  const s = useStyles();
  return (
    <div
      className={mergeClasses(
        s.root,
        "app-card",
        clickable && s.clickable,
        clickable && "app-card--interactive",
        className
      )}
      {...props}
    />
  );
}
