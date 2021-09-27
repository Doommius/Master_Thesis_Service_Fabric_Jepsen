﻿// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;

    [Route("api/[controller]")]
    public class ReliableQueueController : Controller
    {
        private readonly IReliableStateManager StateManager;

        public ReliableQueueController(IReliableStateManager stateManager)
        {
            this.StateManager = stateManager;
        }

        // get api/ReliableQueue/ to get Queue Length
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            IReliableQueue<long> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<long>>("myReliableQueue");
            long returnvalue;
            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue = await queue.GetCountAsync(txn);

                
            }
            List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
            result.Add(new KeyValuePair<string, long>("GetCountAsync", returnvalue));
            return this.Json(result);

        }


        // get api/ReliableQueue/ to get Queue Length
        [HttpGet("peek")]
        public async Task<IActionResult> Get(int request)
        {
            IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");

            ConditionalValue<String> returnvalue;


            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue = await queue.TryPeekAsync(txn);
 
            }
            if (returnvalue.HasValue)
            {

                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>("TryPeekAsync", long.Parse(returnvalue.Value)));
                return this.Json(result);
            }
            else
            {
                return new OkResult();
            }
        }

        // delete api/ReliableQueue/ to pop element
        [HttpDelete("")]
        public async Task<IActionResult> get()
        {
           
         IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");
            ConditionalValue<String> returnvalue;
            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue  = await queue.TryDequeueAsync(txn);

                await txn.CommitAsync();



            }
            if (returnvalue.HasValue)
            {
                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>("TryDequeueAsync", long.Parse(returnvalue.Value)));
                return this.Json(result);
            }
            else
            {
                return new AcceptedResult();
            }


        }

        // put api/ReliableQueue/{item} to get Queue item
        [HttpPut("{value}")]
        public async Task<IActionResult> put(String value)
        {
            

            IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                await queue.EnqueueAsync(txn, value);
                await txn.CommitAsync();
            
                
            }

            return new AcceptedResult();



        }

    }
}