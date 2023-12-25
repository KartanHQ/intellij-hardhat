// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

interface Game {
    //Event after a new Account has ben registered
    event AccountCreation(address accountAddress);

    //Event for depositing new funds
    event AccountDeposit(address accountAddress, uint oldBalance, uint newBalance, uint balanceAdded);

    //Event for withdrawing funds
    event AccountWithdraw(address accountAddress, uint oldBalance, uint newBalance, uint balanceWithdrawed);

}
