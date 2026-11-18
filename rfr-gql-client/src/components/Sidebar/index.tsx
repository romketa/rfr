import {Box, List} from "@mui/material";
import {FC} from "react";
import AccountCircleRoundedIcon from '@mui/icons-material/AccountCircleRounded';
import PersonSearchRoundedIcon from '@mui/icons-material/PersonSearchRounded';
import PublicRoundedIcon from '@mui/icons-material/PublicRounded';
import {SidebarItem} from "./SidebarItem";
import {DrawerHeader} from "../Drawer/DrawerHeader";
import {Drawer} from "../Drawer";

interface SidebarProps {
    sidebarState: boolean,
}

export const Sidebar: FC<SidebarProps> = ({sidebarState}) => {
    return (
        <Drawer
            anchor="left"
            open={sidebarState}
            variant="permanent"
            data-test-id="sidebar"
            sx={{
                '& .MuiDrawer-paper': {
                    bgcolor: "background.paper",
                    borderRight: 1,
                    borderColor: "divider",
                }
            }}
        >
            <DrawerHeader/>
            <Box sx={{overflow: "auto"}}>
                <List sx={{px: 1}}>
                    <SidebarItem
                        sidebarState={sidebarState}
                        name="My map"
                        icon={<PublicRoundedIcon/>}
                        link="/my-travels"
                    />
                    <SidebarItem
                        sidebarState={sidebarState}
                        name="People"
                        icon={<PersonSearchRoundedIcon/>}
                        link="/people"
                    />
                    <SidebarItem
                        sidebarState={sidebarState}
                        name="Profile"
                        icon={<AccountCircleRoundedIcon/>}
                        link="/profile"
                    />
                </List>
            </Box>
        </Drawer>
    );
};
